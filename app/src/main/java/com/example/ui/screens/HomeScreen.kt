package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Drama
import com.example.ui.components.TvDramaCard
import com.example.ui.theme.HongguoGold
import com.example.ui.theme.HongguoRed
import com.example.ui.theme.TvBackground
import com.example.ui.theme.TvCardBorder
import com.example.ui.theme.TvFocusBorder
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import com.example.util.tvFocusable

@Composable
fun HomeScreen(
    dramas: List<Drama>,
    onDramaSelected: (Drama) -> Unit,
    modifier: Modifier = Modifier
) {
    val heroDrama = dramas.firstOrNull()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TvBackground),
        contentPadding = PaddingValues(bottom = 48.dp)
    ) {
        // Hero Featured Banner
        if (heroDrama != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(310.dp)
                        .padding(horizontal = 32.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(TvSurface)
                        .border(1.dp, TvCardBorder, RoundedCornerShape(20.dp))
                ) {
                    // Backdrop Image
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(heroDrama.coverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = heroDrama.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Cinematic Dark Gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.95f),
                                        Color.Black.copy(alpha = 0.75f),
                                        Color.Transparent
                                    ),
                                    startX = 0f,
                                    endX = 1200f
                                )
                            )
                    )

                    // Hero Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 36.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(HongguoRed)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = heroDrama.tag,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Rating",
                                    tint = HongguoGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "%.1f 分".format(heroDrama.rating),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HongguoGold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "全${heroDrama.totalEpisodes}集 · ${heroDrama.category}",
                                fontSize = 13.sp,
                                color = TvTextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = heroDrama.title,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = TvTextPrimary,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = heroDrama.description,
                            fontSize = 14.sp,
                            color = TvTextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(520.dp),
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Remote Action Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(HongguoRed)
                                .tvFocusable(
                                    shape = RoundedCornerShape(12.dp),
                                    focusedBorderColor = Color.White,
                                    focusedBorderWidth = 3.dp,
                                    scaleOnFocus = 1.08f,
                                    onClick = { onDramaSelected(heroDrama) }
                                )
                                .padding(horizontal = 28.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "播放",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "立即看剧",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 1: 今日热门短剧
        item {
            SectionHeader(title = "🔥 今日热门短剧", subtitle = "红果实时热播排行榜")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(dramas) { drama ->
                    TvDramaCard(drama = drama, onClick = { onDramaSelected(drama) })
                }
            }
        }

        // Section 2: 逆袭爽剧精选
        val nixiDramas = dramas.filter { it.category == "穿越逆袭" || it.category == "都市异能" }
        if (nixiDramas.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(title = "⚡ 逆袭异能精选", subtitle = "开局逆袭·打脸爽翻天")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(nixiDramas) { drama ->
                        TvDramaCard(drama = drama, onClick = { onDramaSelected(drama) })
                    }
                }
            }
        }

        // Section 3: 豪门战神系列
        val warDramas = dramas.filter { it.category == "战神归来" || it.category == "豪门恩怨" }
        if (warDramas.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(title = "👑 豪门战神系列", subtitle = "龙王归来·万邦来朝")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(warDramas) { drama ->
                        TvDramaCard(drama = drama, onClick = { onDramaSelected(drama) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TvTextPrimary
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = TvTextSecondary
        )
    }
}
