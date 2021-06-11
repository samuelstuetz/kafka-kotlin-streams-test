package sam.example.accounts.repositories

import kotlin.math.absoluteValue
import kotlin.random.Random

/**
 * placeholder class for basic id generation as I don't use a relational db, cannot just use a seq id.
 * will just use uuid and return a long
 * In distributed production setup a service, database or something should return good uuids.
 *   or just manage accounts in a traditional relational database
 *   this whole KTable idea was more and experiment. Given the requirments
 *     a simple relation db would be easier to build a reliable solution on and simpler
 */
object IdSource {
    private var seed: Int = Random.nextInt()
        set(newSeed:Int) {
            field = newSeed
        }
    private val source by lazy { Random(seed) }
    @Synchronized
    fun newId() = source.nextLong().absoluteValue
}