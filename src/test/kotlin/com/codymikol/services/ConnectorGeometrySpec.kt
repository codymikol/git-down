package com.codymikol.services

import com.codymikol.data.map.MapPoint
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class ConnectorGeometrySpec : DescribeSpec({

    describe("ConnectorGeometry") {

        describe("path") {

            it("runs the vertical down the child lane and stops the mainline gap above the parent") {
                val path = ConnectorGeometry.path(
                    childX = 20f, childY = 24f,
                    parentX = 200f, parentY = 120f,
                    mainlineGap = 24f, forkCurveTension = 1f,
                )

                path.start shouldBe MapPoint(20f, 24f)
                // synthetic node: still in the child's lane, 24px above the parent node
                path.bend shouldBe MapPoint(20f, 96f)
                path.end shouldBe MapPoint(200f, 120f)
            }

            it("puts the bezier control at the sharp corner when tension is full") {
                val path = ConnectorGeometry.path(
                    childX = 20f, childY = 24f,
                    parentX = 200f, parentY = 120f,
                    mainlineGap = 24f, forkCurveTension = 1f,
                )

                // a crisp right-angle: the bend continues straight down to the parent's row
                path.control shouldBe MapPoint(20f, 120f)
            }

            it("relaxes the bezier control toward the midpoint as tension drops to zero") {
                val path = ConnectorGeometry.path(
                    childX = 20f, childY = 24f,
                    parentX = 200f, parentY = 120f,
                    mainlineGap = 24f, forkCurveTension = 0f,
                )

                // midpoint of the fork segment (bend -> end): ((20+200)/2, (96+120)/2)
                path.control shouldBe MapPoint(110f, 108f)
                path.isSameLane shouldBe false
            }

            it("degenerates to a straight vertical for a same-lane connector") {
                val path = ConnectorGeometry.path(
                    childX = 20f, childY = 24f,
                    parentX = 20f, parentY = 120f,
                    mainlineGap = 24f, forkCurveTension = 0.85f,
                )

                path.isSameLane shouldBe true
                path.control.x shouldBe 20f
                path.bend shouldBe MapPoint(20f, 96f)
            }

            it("never lets the vertical rise above the child when the gap exceeds the row span") {
                val path = ConnectorGeometry.path(
                    childX = 20f, childY = 100f,
                    parentX = 200f, parentY = 120f,
                    mainlineGap = 64f, forkCurveTension = 1f,
                )

                // parentY - gap = 56 would sit above the child at y=100; clamp to childY
                path.bend shouldBe MapPoint(20f, 100f)
            }
        }
    }
})
