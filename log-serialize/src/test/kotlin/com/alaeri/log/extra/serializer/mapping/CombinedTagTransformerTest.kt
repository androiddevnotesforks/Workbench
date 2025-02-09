package com.alaeri.log.extra.serializer.mapping

import com.alaeri.log.core.child.ChildTag
import com.alaeri.log.core.context.ListTag
import com.alaeri.log.core.Log.Tag
import com.alaeri.log.extra.identity.IdentityRepresentation
import com.alaeri.log.serialize.serialize.EmptySerializedTag
import com.alaeri.log.serialize.serialize.Identity
import com.alaeri.log.serialize.serialize.Representation
import com.alaeri.log.serialize.serialize.SerializedTag
import com.alaeri.log.serialize.serialize.mapping.CombinedTagTransformer
import com.alaeri.log.serialize.serialize.mapping.TagTypedSerializer
import com.alaeri.log.serialize.serialize.representation.FiliationRepresentation
import com.alaeri.log.serialize.serialize.representation.ListRepresentation
import com.nhaarman.mockitokotlin2.*
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Created by Emmanuel Requier on 18/12/2020.
 */
class CombinedTagTransformerTest {

    @Test
    fun `test childLogContext mapping`(){
        val listIdentity =  IdentityRepresentation(1,"1")
        val combinedLogDataTransformer = CombinedTagTransformer(listOf(), mock {
            onBlocking { transform(any()) } doReturn listIdentity
        })
        val emptyMock: Tag = mock {  }
        val childLogContext : ChildTag = mock { on{parentTag}doReturn emptyMock }
        val childLogRepresentation = combinedLogDataTransformer.transform(childLogContext)
        assertEquals(FiliationRepresentation::class.java, childLogRepresentation.javaClass)
    }

    @Test
    fun `test list mapping`(){
        val listIdentity = IdentityRepresentation(3,"3")
        val combinedLogDataTransformer = CombinedTagTransformer(listOf(), mock {
            onBlocking { transform(any()) } doReturn listIdentity
        })
        val emptyMock: Tag = mock {  }
        val listLogContext : ListTag = mock { on{list}doReturn listOf(emptyMock) }
        val listLogRepresentation = combinedLogDataTransformer.transform(listLogContext)
        assertEquals(ListRepresentation::class.java, listLogRepresentation.javaClass)
    }

    @Test
    fun `test inner mapping works`(){
        val emptyLogRepresentation = EmptySerializedTag(IdentityRepresentation(1, "1"))
        val logTypedMapper : TagTypedSerializer<Tag, SerializedTag> = mock {
            onBlocking{transform(any())} doReturn emptyLogRepresentation
            onBlocking{transformOrNull(any())} doReturn emptyLogRepresentation
        }
        val emptyLogRepresentation2 = EmptySerializedTag(IdentityRepresentation(4, "4"))
        val logTypedMapper2 : TagTypedSerializer<Tag, SerializedTag> = mock {
            onBlocking{transform(any())} doReturn emptyLogRepresentation2
            onBlocking{transformOrNull(any())} doReturn emptyLogRepresentation2
        }
        val listIdentity = IdentityRepresentation(3,"3")
        val combinedLogDataTransformer = CombinedTagTransformer(listOf(logTypedMapper, logTypedMapper2), mock {
            onBlocking { transform(any()) } doReturn listIdentity
        })
        val tag: Tag = mock { }
        val mapped = combinedLogDataTransformer.transform(tag)
        verify(logTypedMapper).transformOrNull(eq(tag))
        verify(logTypedMapper2).transformOrNull(eq(tag))
        //val listIdentity = IdentityRepresentation(3,"4")
        assertEquals(ListRepresentation(listOf(emptyLogRepresentation, emptyLogRepresentation2), listIdentity), mapped)
    }


}