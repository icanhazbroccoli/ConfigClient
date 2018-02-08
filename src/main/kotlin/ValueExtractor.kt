interface ValueExtractor <V, R> {
    fun extractValue(rawData : R) : V
}