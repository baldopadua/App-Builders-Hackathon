package com.secondlook.check

import com.secondlook.ai.model.EmbeddingModel
import com.secondlook.ai.model.WordPieceTokenizer
import java.io.File

fun main(args: Array<String>) {
    require(args.size == 2)
    val vocab = File("../assets/models/embedding/vocab.txt").readLines().filter { it.isNotEmpty() }
    val tokenizer = WordPieceTokenizer(vocab)
    EmbeddingModel(File(args[0]).readBytes(), vocab).use { embedder ->
        for (line in File(args[1]).readLines().drop(1)) {
            val (id, text) = line.split('\t', limit = 2)
            println("TOKENS\t$id\t${tokenizer.encode(text).inputIds.joinToString(",")}")
            println("PARITY\t$id\t${embedder.embed(text).joinToString(",")}")
        }
    }
}
