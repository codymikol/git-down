package com.codymikol.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.ButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import com.codymikol.data.Colors
import com.codymikol.gitdown.generated.resources.Res
import com.codymikol.gitdown.generated.resources.commit
import com.codymikol.gitdown.generated.resources.commit_white
import com.codymikol.state.GitDownState
import com.codymikol.tabs.Tab
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val activeTabBackground = Color(200, 200, 200)
private val inactiveTabBackground = Color(106,106,106)

class TabButtonColors(val selected: Boolean) : ButtonColors {

    private fun getButtonBackground(): Color = when (selected) {
        true -> Colors.LightGrayText
        false -> Colors.LightGrayBackground
    }

    @Composable
    override fun backgroundColor(enabled: Boolean): State<Color> =
        mutableStateOf(getButtonBackground())

    @Composable
    override fun contentColor(enabled: Boolean): State<Color> = mutableStateOf(Colors.LightGrayText)
}

enum class TabButtonLocation {
    Left,
    Right,
    Middle,
}

val cornerSize = 6.dp

private fun getTabButtonShape(location: TabButtonLocation): AbsoluteRoundedCornerShape =
    when (location) {
        TabButtonLocation.Left -> AbsoluteRoundedCornerShape(
            CornerSize(cornerSize),
            CornerSize(0.dp),
            CornerSize(0.dp),
            CornerSize(cornerSize)
        )
        TabButtonLocation.Middle -> AbsoluteRoundedCornerShape(
            CornerSize(0.dp),
            CornerSize(0.dp),
            CornerSize(0.dp),
            CornerSize(0.dp)
        )
        TabButtonLocation.Right -> AbsoluteRoundedCornerShape(
            CornerSize(0.dp),
            CornerSize(cornerSize),
            CornerSize(cornerSize),
            CornerSize(0.dp)
        )
    }

@Composable
fun tabButton(
    location: TabButtonLocation,
    tab: Tab,
    enabledIconSrc: DrawableResource,
    disabledIconSrc: DrawableResource,
    description: String
) {
    Box(
        modifier = Modifier
            .width(38.dp)
            .height(28.dp)
            .clickable { GitDownState.selectTab(tab) }
            .clip(getTabButtonShape(location))
    ) {
        Box(
            modifier = Modifier.background(if(tab == GitDownState.currentTab.value) activeTabBackground else inactiveTabBackground)
        ) {
            Image(
                modifier = Modifier.scale(0.5f).fillMaxSize(),
                painter = painterResource(if(tab == GitDownState.currentTab.value) enabledIconSrc else disabledIconSrc),
                contentDescription = description,
            )
        }
    }
}

@Preview
@Composable
fun previewButton() {
    tabButton(
        TabButtonLocation.Left,
        Tab.Commit,
        Res.drawable.commit,
        Res.drawable.commit_white,
        "lol"
    )
}