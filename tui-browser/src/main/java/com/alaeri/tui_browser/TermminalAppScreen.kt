package com.alaeri.tui_browser

import com.alaeri.tui_browser.wiki.LoadingStatus
import com.alaeri.tui_browser.wiki.WikiArticle
import com.alaeri.tui_browser.wiki.WikiText
import com.googlecode.lanterna.SGR
import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.*
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import com.googlecode.lanterna.screen.Screen
import com.googlecode.lanterna.terminal.Terminal
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

class TermminalAppScreen(terminal: Terminal,
                         private val screen: Screen,
                         private val logger: ILogger,
                         private val viewModelFactory: ViewModelFactory,
                         private val drawCoroutineContext : CoroutineContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher(),
                         private val readKeyCoroutineContext : CoroutineContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {

    private var rootWindow: MultiWindowTextGUI
    private val textBox: TextBox
    private val mainContentPanelLeft: Panel
    private val lineWrapper = LineWrapper()

    init {
        screen.startScreen()
        rootWindow = MultiWindowTextGUI(screen).apply {

        }
        val browseWindow = BasicWindow("Browse").apply {
            setHints(listOf(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS))
            rootWindow.addWindow(this)

        }

        //browseWindow.textGUI = rootWindow
        val linearLayout = LinearLayout()
        val windowPanel = Panel(linearLayout).apply {
            preferredSize = screen.terminalSize

        }
        browseWindow.component = windowPanel
        val gridLayout = GridLayout(2).apply {
        }
        val mainContentPanel = Panel(gridLayout).apply {
            addTo(windowPanel)
            preferredSize = parent.preferredSize.withRelativeRows(-1)
        }
        mainContentPanelLeft = Panel(AbsoluteLayout()).apply {
            addTo(mainContentPanel)
            preferredSize = parent.preferredSize.withColumns(parent.preferredSize.columns / 2)
        }
        mainContentPanel.addComponent(Label("TEST2").apply {
            addTo(mainContentPanel)
            preferredSize = parent.preferredSize.withColumns(parent.preferredSize.columns / 2)
        })
        val lastRowLayout = LinearLayout(Direction.HORIZONTAL)
        val lastRowPanel = Panel(lastRowLayout).apply {
            addTo(windowPanel)
            preferredSize = parent.preferredSize.withRows(1)
        }
        windowPanel.addComponent(
            lastRowPanel,
            LinearLayout.createLayoutData(LinearLayout.Alignment.End)
        )
        lastRowPanel.addComponent(Label("search"))
        textBox = TextBox().apply {
            setTextChangeListener { newText, changedByUserInteraction ->

            }
        }
        lastRowPanel.addComponent(textBox)
        rootWindow.updateScreen()
    }

    suspend fun emit(combined: PresentationState.Presentation) {

        val inputState = combined.inputState
        val contentStatus = combined.contentStatus
        val selectedWikiText = combined.selectedWikiText
        logger.println("combined")

        textBox.text = inputState.text

        when (contentStatus) {
            is LoadingStatus.Done -> {
                //data class CursorPosStart(val x: Int, val y: Int)
                contentStatus.result.lines.forEach { logger?.println(it) }
                //val textGraphics = screen.newTextGraphics()
                printPage(mainContentPanelLeft, contentStatus, selectedWikiText)
            }
            else -> {
                val fakePage = WikiArticle(
                    "",
                    "",
                    mutableListOf(
                        mutableListOf<WikiText>(
                            WikiText.NormalText(
                                contentStatus.toString()
                            )
                        )
                    )
                )
                printPage(mainContentPanelLeft, LoadingStatus.Done(fakePage))
            }
        }
        rootWindow.updateScreen()



    }

    private val keyFlow: Flow<KeyStroke> = flow<KeyStroke> {
        val emissionContext = currentCoroutineContext()
        withContext(readKeyCoroutineContext) {
            var keyStroke = screen.readInput()
            while (true) {
                withContext(emissionContext) {
                    emit(keyStroke)
                }
                keyStroke = screen.readInput()
            }
        }
    }

    private val sizeFlow: Flow<TerminalSize> = callbackFlow<TerminalSize> {
        terminal.addResizeListener { _, newSize -> this.offer(newSize) }
    }

    private fun printPage(
        mainContentPanelLeft: Panel,
        contentStatus: LoadingStatus.Done,
        selectedPosition: WikiText.InternalLink? = null
    ) {
        mainContentPanelLeft.removeAllComponents()
        val maxColumns = mainContentPanelLeft.size.columns
        val maxHeight = mainContentPanelLeft.size.rows
        printV1(
            contentStatus,
            maxColumns,
            maxHeight,
            mainContentPanelLeft,
            selectedPosition
        )
        //printV2(contentStatus, maxColumns, maxHeight, mainContentPanelLeft)

    }

    private fun printV1(
        contentStatus: LoadingStatus.Done,
        maxColumns: Int,
        maxHeight: Int,
        mainContentPanelLeft: Panel,
        selectedWikiText: WikiText.InternalLink?
    ) {
        val startPos = TerminalPosition(0, 0)
        val endPos = TerminalPosition(maxColumns, maxHeight)
        contentStatus.result.lines.fold(startPos) { lineStartPos, line ->
            if (lineStartPos.row < endPos.row) {
                val result =
                    lineWrapper.chunkAndWrapElements(
                        line,
                        lineStartPos,
                        maxColumns
                    )
                result.chunks.filter { it.position.row < maxHeight }.forEach {
                    val label = Label(it.text.text).apply {
                        position = it.position
                        size = TerminalSize(it.text.text.length, 1)
                        if (it.text is WikiText.InternalLink) {
                            addStyle(SGR.BOLD)
                            if (it.text.target == selectedWikiText?.target) {
                                addStyle(SGR.REVERSE)
                            }
                        }
                    }
                    mainContentPanelLeft.addComponent(label)
                }
                TerminalPosition(0, result.lastPosition.row + 1)
            } else {
                lineStartPos
            }
        }
    }

    suspend fun runAppAndWait() {

        supervisorScope {
            val instantiationScope = CoroutineScope(currentCoroutineContext())
            try{
                val sharedTerminalScreen =  SharedTerminalScreen(
                    this@TermminalAppScreen.keyFlow,
                    this@TermminalAppScreen.sizeFlow,
                    instantiationScope)
                val viewModel = viewModelFactory.provideViewModel(
                    sharedTerminalScreen,
                    instantiationScope)

                val executionJob = launch {
                    viewModel.screenState.flowOn(drawCoroutineContext).collect {
                        when(it){
                            is PresentationState.Presentation -> this@TermminalAppScreen.emit(it)
                            else -> instantiationScope.cancel()
                        }

                    }
                }
                executionJob.join()
            }catch (e: Exception){
                logger.println(e)
            }
            finally {
                screen.stopScreen()
            }
        }
    }
}
