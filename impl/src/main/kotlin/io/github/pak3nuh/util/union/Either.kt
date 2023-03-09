package io.github.pak3nuh.util.union

sealed class Either<T1, T2> {

    val isFirst: Boolean get() = this is First

    val isSecond: Boolean get() = this is Second

    fun <OUT> fold(firstMapper: (T1) -> OUT, secondMapper: (T2) -> OUT): OUT {
        return when (this) {
            is First -> firstMapper(this.first)
            is Second -> secondMapper(this.second)
        }
    }

    data class First<T1, T2>(val first: T1): Either<T1, T2>()
    data class Second<T1, T2>(val second: T2): Either<T1, T2>()

}