package studio1a23.altTextAi.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import studio1a23.altTextAi.R

enum class StepState {
    Active,
    Success,
    Fail,
    Upcoming
}

@Composable
fun VerticalStepper(modifier: Modifier = Modifier, content: (@Composable () -> Unit)?) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        content?.invoke()
    }
}

@Composable
fun StepItem(
    title: @Composable () -> Unit,
    description: (@Composable () -> Unit)? = null,
    state: StepState,
    stepNumber: Int,
    content: (@Composable () -> Unit)? = null
) {
    val circleColor = when (state) {
        StepState.Fail -> MaterialTheme.colorScheme.error
        StepState.Success, StepState.Active ->
            MaterialTheme.colorScheme.primary
        StepState.Upcoming -> MaterialTheme.colorScheme.primaryContainer
    }
    val stageDescription = stringResource(R.string.step_content_description, stepNumber, when(state) {
        StepState.Fail -> R.string.step_state_failed
        StepState.Success -> R.string.step_state_completed
        StepState.Active -> R.string.step_state_active
        StepState.Upcoming -> R.string.step_state_upcoming
    })

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        // Step indicator column (circle and line)
        Column(modifier = Modifier
            .width(40.dp)
            .fillMaxHeight()) {
            // Step circle
            Surface(
                shape = CircleShape,
                color = circleColor,
                modifier =
                Modifier
                    .size(28.dp)
                    .align(Alignment.CenterHorizontally)
                    .semantics {
                        contentDescription = stageDescription
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    when (state) {
                        StepState.Fail ->
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.size(16.dp)
                            )

                        StepState.Success ->
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )

                        StepState.Active, StepState.Upcoming ->
                            Text(
                                text = stepNumber.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                color =
                                when (state) {
                                    StepState.Active -> MaterialTheme.colorScheme.onPrimary
                                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            )
                    }
                }
            }
            VerticalDivider(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 4.dp),
                thickness = 1.dp,
                color =
                when (state) {
                    StepState.Success -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        }

        // Content column
        Box(modifier = Modifier
            .weight(1f)
            .padding(start = 16.dp)) {
            // Content
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    ProvideTextStyle(
                        value =
                        MaterialTheme.typography.titleMedium.copy(
                            lineBreak = LineBreak.Heading,
                            color =
                            when (state) {
                                StepState.Fail ->
                                    MaterialTheme.colorScheme.error

                                StepState.Active ->
                                    MaterialTheme.colorScheme.primary

                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    ) { title() }
                }

                if (description != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        ProvideTextStyle(
                            value =
                            MaterialTheme.typography.bodyMedium.copy(
                                lineBreak = LineBreak.Heading,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) { description() }
                    }
                }

                if (content != null) {
                    AnimatedVisibility(
                        visible = state == StepState.Active,
                        enter = expandVertically(expandFrom = Alignment.Top),
                        exit = shrinkVertically(shrinkTowards = Alignment.Top)
                    ) { Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)) { content() } }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VerticalStepperPreview() {
    MaterialTheme {
        Surface {
            VerticalStepper(modifier = Modifier.padding(16.dp)) {
                StepItem(
                    title = { Text("Step 1") },
                    description = { Text("This is a completed step") },
                    state = StepState.Success,
                    stepNumber = 1,
                    content = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "This content will not be visible because the step is completed"
                            )
                            Button(onClick = {}) { Text("Continue") }
                        }
                    }
                )
                StepItem(
                    title = { Text("Step 2") },
                    description = { Text("This is the current step") },
                    state = StepState.Active,
                    stepNumber = 2,
                    content = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = "Example input",
                                onValueChange = {},
                                label = { Text("Input field") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = {}) { Text("Skip") }
                                Button(onClick = {}) { Text("Continue") }
                            }
                        }
                    }
                )
                StepItem(
                    title = { Text("Step 3") },
                    description = { Text("This step has an error") },
                    state = StepState.Fail,
                    stepNumber = 3,
                    content = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("This content will not be visible because the step failed")
                            Button(onClick = {}) { Text("Retry") }
                        }
                    }
                )
                StepItem(
                    title = { Text("Step 4") },
                    description = { Text("This is an upcoming step") },
                    state = StepState.Upcoming,
                    stepNumber = 4,
                    content = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "This content will not be visible because the step is upcoming"
                            )
                            Button(onClick = {}) { Text("Start") }
                        }
                    }
                )
                StepItem(
                    title = { Text("Step 5") },
                    state = StepState.Upcoming,
                    stepNumber = 5,
                    content = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "This content will not be visible because the step is upcoming"
                            )
                            Button(onClick = {}) { Text("Start") }
                        }
                    }
                )
            }
        }
    }
}
