package com.codymikol.components.commit.diff.file.header

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codymikol.components.commit.diff.file.header.action.FileHeaderActions
import com.codymikol.components.commit.diff.file.header.icons.FileHeaderIcon
import com.codymikol.components.commit.diff.file.header.text.FileHeaderText
import com.codymikol.data.diff.FileDeltaNode

@Composable
fun FileHeader(fileDeltaNode: FileDeltaNode) = Row(
    modifier = Modifier.height(32.dp)
        .background(fileDeltaNode.fileDelta.color)
        .fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
) {

    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween) {

        Row(
            modifier = Modifier.fillMaxHeight().weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Spacer(modifier = Modifier.width(6.dp))

            FileHeaderIcon(fileDeltaNode)

            Spacer(modifier = Modifier.width(6.dp))

            FileHeaderText(fileDeltaNode)

        }

        Row(
            modifier = Modifier.fillMaxHeight().width(100.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {

            FileHeaderActions(fileDeltaNode)

            Spacer(modifier = Modifier.width(6.dp))
        }
    }

}