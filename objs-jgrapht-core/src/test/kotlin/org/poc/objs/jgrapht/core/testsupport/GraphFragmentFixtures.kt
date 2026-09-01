package org.poc.objs.jgrapht.core.testsupport

import org.poc.objs.api.domain.DefaultGraphFragmentPolicy
import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.GraphContents
import org.poc.objs.api.domain.ResolvedGraphFragment
import java.util.UUID

object GraphFragmentFixtures {
    fun twoNodeCycle(): ResolvedGraphFragment {
        val a = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val b = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val ab = UUID.fromString("00000000-0000-0000-0000-000000000101")
        val ba = UUID.fromString("00000000-0000-0000-0000-000000000102")
        return DefaultGraphFragmentPolicy.resolve(
            GraphContents(
                entities = listOf(
                    Entity(a, "Component", "1"),
                    Entity(b, "Component", "1"),
                ),
                edges = listOf(
                    Edge(ab, source = a, target = b, role = "depends_on"),
                    Edge(ba, source = b, target = a, role = "depends_on"),
                ),
            ),
        )
    }

    fun selfLoop(): ResolvedGraphFragment {
        val a = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val loop = UUID.fromString("00000000-0000-0000-0000-000000000101")
        return DefaultGraphFragmentPolicy.resolve(
            GraphContents(
                entities = listOf(Entity(a, "Component", "1")),
                edges = listOf(Edge(loop, source = a, target = a, role = "depends_on")),
            ),
        )
    }

    fun acyclic(): ResolvedGraphFragment {
        val a = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val b = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val ab = UUID.fromString("00000000-0000-0000-0000-000000000101")
        return DefaultGraphFragmentPolicy.resolve(
            GraphContents(
                entities = listOf(
                    Entity(a, "Component", "1"),
                    Entity(b, "Component", "1"),
                ),
                edges = listOf(Edge(ab, source = a, target = b, role = "depends_on")),
            ),
        )
    }
}
