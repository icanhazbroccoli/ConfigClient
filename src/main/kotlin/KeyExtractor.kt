interface KeyExtractor <K, R> {
    fun extractKey(rawData : R) : K
}