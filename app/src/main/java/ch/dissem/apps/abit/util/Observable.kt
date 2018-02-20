package ch.dissem.apps.abit.util

import kotlin.properties.Delegates

/**
 * A simple observable implementation that should be mostly
 */
class Observable<T>(value: T) {
    private val observers = mutableMapOf<Any, (T) -> Unit>()

    var value: T by Delegates.observable(value, { _, old, new ->
        if (old != new) {
            observers.values.forEach { it.invoke(new) }
        }
    })

    /**
     * The key will make sure the observer can easily be removed. Usually the key should be either
     * the object that created the observer, or the observer itself, if it's easily available.
     *
     * Note that a map is used for observers, so if you define more than one observer with the same
     * key, all previous ones will be removed. Also, the observers will be notified in no specific
     * order.
     *
     * To prevent memory leaks, the observer must be removed if it isn't used anymore.
     */
    fun addObserver(key: Any, observer: (T) -> Unit) {
        observers[key] = observer
    }

    /**
     * Remove the observer that was registered with the given key.
     */
    fun removeObserver(key: Any) {
        observers.remove(key)
    }
}
