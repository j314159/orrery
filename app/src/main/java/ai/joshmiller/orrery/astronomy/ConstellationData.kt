package ai.joshmiller.orrery.astronomy

/**
 * A constellation defined by its name and the star-pair line segments
 * that form its stick-figure pattern. Star names must match entries
 * in BRIGHT_STARS exactly.
 */
data class Constellation(
    val name: String,
    val lines: List<Pair<String, String>>
)

/**
 * The most recognizable constellations, drawn using only stars from our catalog.
 * Includes the zodiac (where we have enough stars) plus major northern and
 * southern constellations.
 */
val CONSTELLATIONS: List<Constellation> = listOf(

    // === ZODIAC ===

    Constellation("Aries", listOf(
        "Hamal" to "Sheratan"
    )),

    Constellation("Taurus", listOf(
        "Aldebaran" to "Elnath",
        "Aldebaran" to "Tianguan",
        // Pleiades cluster
        "Alcyone" to "Atlas",
        "Alcyone" to "Electra",
        "Alcyone" to "Maia",
        "Alcyone" to "Merope"
    )),

    Constellation("Gemini", listOf(
        "Castor" to "Pollux",
        "Pollux" to "Alhena"
    )),

    // Cancer — too faint, no stars in catalog
    // Pisces — too faint, no stars in catalog

    Constellation("Leo", listOf(
        "Regulus" to "Algieba",
        "Regulus" to "Denebola",
        "Algieba" to "Denebola"
    )),

    Constellation("Virgo", listOf(
        "Spica" to "Porrima",
        "Porrima" to "Vindemiatrix"
    )),

    Constellation("Libra", listOf(
        "Zubenelgenubi" to "Zubeneschamali"
    )),

    Constellation("Scorpius", listOf(
        "Acrab" to "Dschubba",
        "Dschubba" to "Antares",
        "Antares" to "Larawag",
        "Larawag" to "Sargas",
        "Sargas" to "Shaula"
    )),

    Constellation("Sagittarius", listOf(
        "Kaus Australis" to "Nunki"
    )),

    Constellation("Capricornus", listOf(
        "Dabih" to "Nashira",
        "Nashira" to "Deneb Algedi"
    )),

    Constellation("Aquarius", listOf(
        "Sadalmelik" to "Sadalsuud"
    )),

    // === MAJOR CONSTELLATIONS ===

    Constellation("Orion", listOf(
        "Betelgeuse" to "Bellatrix",
        "Bellatrix" to "Mintaka",
        "Mintaka" to "Alnilam",
        "Alnilam" to "Alnitak",
        "Alnitak" to "Saiph",
        "Mintaka" to "Rigel",
        "Betelgeuse" to "Alnilam"
    )),

    Constellation("Ursa Major", listOf(
        "Alkaid" to "Mizar",
        "Mizar" to "Alioth",
        "Alioth" to "Megrez",
        "Megrez" to "Phecda",
        "Phecda" to "Merak",
        "Merak" to "Dubhe",
        "Dubhe" to "Megrez"
    )),

    Constellation("Ursa Minor", listOf(
        "Polaris" to "Yildun",
        "Kochab" to "Pherkad"
    )),

    Constellation("Cassiopeia", listOf(
        "Segin" to "Ruchbah",
        "Ruchbah" to "Navi",
        "Navi" to "Schedar",
        "Schedar" to "Caph"
    )),

    Constellation("Cygnus", listOf(
        "Deneb" to "Sadr",
        "Sadr" to "Albireo",
        "Sadr" to "Aljanah"
    )),

    Constellation("Lyra", listOf(
        "Vega" to "Sheliak",
        "Vega" to "Sulafat",
        "Sheliak" to "Sulafat"
    )),

    Constellation("Aquila", listOf(
        "Altair" to "Tarazed"
    )),

    Constellation("Canis Major", listOf(
        "Sirius" to "Mirzam",
        "Sirius" to "Adhara",
        "Adhara" to "Wezen",
        "Adhara" to "Furud",
        "Wezen" to "Aludra"
    )),

    Constellation("Pegasus", listOf(
        "Markab" to "Scheat",
        "Scheat" to "Alpheratz",
        "Markab" to "Enif"
    )),

    Constellation("Andromeda", listOf(
        "Alpheratz" to "Mirach",
        "Mirach" to "Almach"
    )),

    Constellation("Draco", listOf(
        "Eltanin" to "Rastaban",
        "Rastaban" to "Thuban",
        "Thuban" to "Altais"
    )),

    Constellation("Perseus", listOf(
        "Mirfak" to "Algol"
    )),

    Constellation("Lepus", listOf(
        "Arneb" to "Nihal"
    ))
)
