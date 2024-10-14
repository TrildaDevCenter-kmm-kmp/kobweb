package playground.worker

import com.varabyte.kobweb.serialization.createIOSerializer
import com.varabyte.kobweb.worker.OutputDispatcher
import com.varabyte.kobweb.worker.WorkerFactory
import com.varabyte.kobweb.worker.WorkerStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SumInputs(val a: Int, val b: Int)

@Serializable
data class SumOutput(val sum: Int)

internal class SumWorkerFactory : WorkerFactory<SumInputs, SumOutput> {
    override fun createStrategy(postOutput: OutputDispatcher<SumOutput>) = WorkerStrategy<SumInputs> { input ->
        postOutput(SumOutput(input.a + input.b))
    }

    override fun createIOSerializer() = Json.createIOSerializer<SumInputs, SumOutput>()
}
