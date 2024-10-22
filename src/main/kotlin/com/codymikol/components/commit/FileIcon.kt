package com.codymikol.components.commit

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codymikol.data.Colors
import com.codymikol.data.file.FileDelta
import com.codymikol.typography.jetbrainsMono

@Composable
fun FileIcon(modifier: Modifier = Modifier, letter: String, color: Color, borderColor: Color) = Column(
    modifier = Modifier.wrapContentSize(Alignment.Center),
) {

    Box(modifier = Modifier
        .size(14.dp)
        .clip(shape = RoundedCornerShape(2.dp))
        .border(width = 0.5.dp, color = borderColor, shape = RoundedCornerShape(2.dp))
        .background(color)
        .then(modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(letter, fontFamily = jetbrainsMono(), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color.White, fontSize = 7.sp)
    }



}

@Composable
fun FileIcon(modifier: Modifier = Modifier, fileDelta: FileDelta) = FileIcon(modifier, fileDelta.letter, fileDelta.color, fileDelta.borderColor)

@Preview
@Composable
fun TestAddedIcon() = Box(modifier = Modifier.width(50.dp).height(50.dp).background(Color.White).padding(15.dp)) { FileIcon(letter = "A", color = Colors.FileAdded, borderColor = Colors.FileAddedBorder) }

@Preview
@Composable
fun TestDeletedIcon() = Box(modifier = Modifier.width(50.dp).height(50.dp).background(Color.White).padding(15.dp)) { FileIcon(letter = "D", color = Colors.FileRemoved, borderColor = Colors.FileRemovedBorder) }

@Preview
@Composable
fun TestModifiedIcon() = Box(modifier = Modifier.width(50.dp).height(50.dp).background(Color.White).padding(15.dp)) { FileIcon(letter = "M", color = Colors.FileModified, borderColor = Colors.FileModifiedBorder) }
