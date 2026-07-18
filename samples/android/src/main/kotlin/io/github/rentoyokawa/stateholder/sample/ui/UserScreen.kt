package io.github.rentoyokawa.stateholder.sample.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rentoyokawa.stateholder.core.StateHolder
import io.github.rentoyokawa.stateholder.sample.feature.UserDetailAction
import io.github.rentoyokawa.stateholder.sample.feature.UserDetailState
import io.github.rentoyokawa.stateholder.sample.feature.UserListAction
import io.github.rentoyokawa.stateholder.sample.feature.UserListState
import io.github.rentoyokawa.stateholder.sample.feature.UserScreenViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.androidx.compose.koinViewModel

@Composable
fun UserScreen(
    viewModel: UserScreenViewModel = koinViewModel(),
) {
    Row(Modifier.fillMaxSize()) {
        UserListPane(
            holder = viewModel.userList,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        VerticalDivider()
        UserDetailPane(
            holder = viewModel.userDetail,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
    }
}

// View は契約（StateHolder<State, Action>）にだけ依存する。
// state を購読し action を呼ぶ、という対称な形。

@Composable
fun UserListPane(
    holder: StateHolder<UserListState, UserListAction>,
    modifier: Modifier = Modifier,
) {
    val state by holder.state.collectAsStateWithLifecycle()
    Column(modifier.padding(16.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = holder.action::search,
            label = { Text("Search") },
            modifier = Modifier.fillMaxWidth(),
        )
        LazyColumn {
            items(state.rows, key = { it.id }) { row ->
                ListItem(
                    headlineContent = { Text(row.name) },
                    leadingContent = {
                        RadioButton(selected = row.isSelected, onClick = null)
                    },
                    modifier = Modifier.clickable { holder.action.select(row.id) },
                )
            }
        }
    }
}

@Composable
fun UserDetailPane(
    holder: StateHolder<UserDetailState, UserDetailAction>,
    modifier: Modifier = Modifier,
) {
    val state by holder.state.collectAsStateWithLifecycle()
    Box(modifier.padding(16.dp)) {
        when (val s = state) {
            UserDetailState.NothingSelected -> Text("ユーザーを選択してください")
            is UserDetailState.Loaded -> Column {
                Text(s.name, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                Text(s.bio, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                Button(onClick = holder.action::toggleFavorite) {
                    Text(if (s.isFavorite) "★ Favorite" else "☆ Add to favorites")
                }
            }
        }
    }
}

// ── Preview: 契約が interface なので fake がその場で作れる ──────

private fun <S, A> fakeHolder(state: S, action: A): StateHolder<S, A> =
    object : StateHolder<S, A> {
        override val state = MutableStateFlow(state)
        override val action = action
    }

@Preview(showBackground = true)
@Composable
private fun UserListPanePreview() {
    MaterialTheme {
        UserListPane(
            holder = fakeHolder(
                state = UserListState(
                    query = "",
                    rows = listOf(
                        UserListState.Row("u1", "Alice", isSelected = true),
                        UserListState.Row("u2", "Bob", isSelected = false),
                    ),
                ),
                action = object : UserListAction {
                    override fun search(query: String) {}
                    override fun select(id: String) {}
                },
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UserDetailPanePreview() {
    MaterialTheme {
        UserDetailPane(
            holder = fakeHolder(
                state = UserDetailState.Loaded(
                    name = "Alice",
                    bio = "Android engineer. Loves Compose.",
                    isFavorite = true,
                ) as UserDetailState,
                action = object : UserDetailAction {
                    override fun toggleFavorite() {}
                },
            ),
        )
    }
}
