package com.urlstream.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.urlstream.model.VideoInfo
import com.urlstream.ui.screens.HomeScreen
import com.urlstream.ui.screens.PlayerScreen
import com.urlstream.viewmodel.MainViewModel

object VideoHolder {
    var currentVideo: VideoInfo? = null
}

object Routes {
    const val HOME = "home"
    const val PLAYER = "player/{videoId}"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onVideoClick = { video ->
                    VideoHolder.currentVideo = video
                    navController.navigate("player/${video.id}")
                }
            )
        }
        composable(
            route = Routes.PLAYER,
            arguments = listOf(
                navArgument("videoId") { type = NavType.IntType }
            )
        ) {
            val video = VideoHolder.currentVideo
            if (video != null) {
                PlayerScreen(
                    video = video,
                    onBack = {
                        VideoHolder.currentVideo = null
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
