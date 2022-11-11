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
import com.codymikol.data.file.FileDelta

@Composable
fun FileHeader(fileDelta: FileDelta) = Row(
    modifier = Modifier.height(32.dp)
        .background(fileDelta.color)
        .fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
) {

    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween) {

        Row(
            modifier = Modifier.fillMaxHeight().wrapContentWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Spacer(modifier = Modifier.width(6.dp))

            FileHeaderIcon(fileDelta)

            Spacer(modifier = Modifier.width(6.dp))

            FileHeaderText(fileDelta)

        }

        Row(
            modifier = Modifier.fillMaxHeight().wrapContentWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            FileHeaderActions(fileDelta)

            Spacer(modifier = Modifier.width(6.dp))
        }
    }

}