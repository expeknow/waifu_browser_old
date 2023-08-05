package com.expeknow.waifubrowser.utils

object Constants {
    const val MIN_DISTANCE = 100
    /**
     * waifu pics
     */
    const val sfwLink1 : String = "https://api.waifu.pics/sfw/"
    /**
     * waifu IM
     */
    const val sfwLink2 : String = "https://api.waifu.im/search?is_nsfw=false&gif=false&orientation=PORTRAIT&many=true&included_tags="
    /**
     * waifu pics
     */
    const val nsfwLink1 : String = "https://api.waifu.pics/nsfw/"
    /**
     * waifu IM
     */
    const val nsfwLink2 : String = "https://api.waifu.im/search?is_nsfw=true&orientation=PORTRAIT&many=true&gif=false&included_tags="

    val SFW_LINK1 = arrayOf<String>("waifu", "neko", "shinobu", "megumin", "bully", "cuddle",
        "cry", "hug", "awoo", "kiss", "lick", "pat", "smug", "bonk", "yeet", "blush", "smile",
        "wave", "highfive", "handhold", "nom", "bite", "glomp", "slap", "kill", "kick", "happy",
        "wink", "poke", "dance", "cringe")

    val NSFW_LINK1 = arrayOf("waifu", "neko", "blowjob", "trap")

    val SFW_LINK2 = arrayOf("waifu", "maid", "marin-kitagawa", "mori-calliope", "raiden-shogun",
        "oppai", "selfies", "uniform")

    val NSFW_LINK2 = arrayOf("waifu", "maid", "marin-kitagawa", "mori-calliope", "raiden-shogun",
        "oppai", "selfies", "uniform", "ass", "hentai", "milf", "oral", "ecchi", "ero", "paizuri")
}
