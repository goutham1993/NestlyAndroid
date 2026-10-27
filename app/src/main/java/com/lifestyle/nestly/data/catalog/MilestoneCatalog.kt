package com.lifestyle.nestly.data.catalog

data class Milestone(val key: String, val title: String)

data class MilestoneGroup(val label: String, val ageMonths: Int, val items: List<Milestone>)

/** Based on the CDC "Learn the Signs. Act Early." developmental milestone checklists. */
object MilestoneCatalog {
    val groups: List<MilestoneGroup> = listOf(
        group(
            "2 months", 2,
            "Calms down when spoken to or picked up",
            "Looks at your face",
            "Seems happy to see you when you walk up to them",
            "Smiles when you talk or smile at them",
            "Makes sounds other than crying",
            "Reacts to loud sounds",
            "Watches you as you move",
            "Looks at a toy for several seconds",
            "Holds head up when on tummy",
            "Moves both arms and both legs",
            "Opens hands briefly",
        ),
        group(
            "4 months", 4,
            "Smiles on their own to get your attention",
            "Chuckles (not yet a full laugh) when you try to make them laugh",
            "Looks at you, moves, or makes sounds to get or keep your attention",
            "Makes sounds like \"oooo\", \"aahh\" (cooing)",
            "Makes sounds back when you talk to them",
            "Turns head towards the sound of your voice",
            "If hungry, opens mouth when they see breast or bottle",
            "Looks at their hands with interest",
            "Holds head steady without support when you are holding them",
            "Holds a toy when you put it in their hand",
            "Uses their arm to swing at toys",
            "Brings hands to mouth",
            "Pushes up onto elbows/forearms when on tummy",
        ),
        group(
            "6 months", 6,
            "Knows familiar people",
            "Likes to look at themselves in a mirror",
            "Laughs",
            "Takes turns making sounds with you",
            "Blows \"raspberries\" (sticks tongue out and blows)",
            "Makes squealing noises",
            "Puts things in their mouth to explore them",
            "Reaches to grab a toy they want",
            "Closes lips to show they don't want more food",
            "Rolls from tummy to back",
            "Pushes up with straight arms when on tummy",
            "Leans on hands to support themselves when sitting",
        ),
        group(
            "9 months", 9,
            "Is shy, clingy, or fearful around strangers",
            "Shows several facial expressions, like happy, sad, angry, and surprised",
            "Looks when you call their name",
            "Reacts when you leave (looks, reaches for you, or cries)",
            "Smiles or laughs when you play peek-a-boo",
            "Makes a lot of different sounds like \"mamamama\" and \"babababa\"",
            "Lifts arms up to be picked up",
            "Looks for objects when dropped out of sight",
            "Bangs two things together",
            "Gets to a sitting position by themselves",
            "Moves things from one hand to the other",
            "Uses fingers to \"rake\" food towards themselves",
            "Sits without support",
        ),
        group(
            "12 months", 12,
            "Plays games with you, like pat-a-cake",
            "Waves \"bye-bye\"",
            "Calls a parent \"mama\" or \"dada\" or another special name",
            "Understands \"no\" (pauses briefly or stops when you say it)",
            "Puts something in a container, like a block in a cup",
            "Looks for things they see you hide",
            "Pulls up to stand",
            "Walks, holding on to furniture",
            "Drinks from a cup without a lid, as you hold it",
            "Picks things up between thumb and pointer finger",
        ),
        group(
            "15 months", 15,
            "Copies other children while playing",
            "Shows you an object they like",
            "Claps when excited",
            "Hugs stuffed doll or other toy",
            "Shows you affection (hugs, cuddles, or kisses you)",
            "Tries to say one or two words besides \"mama\" or \"dada\"",
            "Looks at a familiar object when you name it",
            "Follows directions given with both a gesture and words",
            "Points to ask for something or to get help",
            "Tries to use things the right way, like a phone, cup, or book",
            "Stacks at least two small objects, like blocks",
            "Takes a few steps on their own",
            "Uses fingers to feed themselves some food",
        ),
        group(
            "18 months", 18,
            "Moves away from you, but looks to make sure you are close by",
            "Points to show you something interesting",
            "Puts hands out for you to wash them",
            "Looks at a few pages in a book with you",
            "Helps you dress them by pushing arm through sleeve or lifting up foot",
            "Tries to say three or more words besides \"mama\" or \"dada\"",
            "Follows one-step directions without any gestures",
            "Copies you doing chores, like sweeping with a broom",
            "Plays with toys in a simple way, like pushing a toy car",
            "Walks without holding on to anyone or anything",
            "Scribbles",
            "Drinks from a cup without a lid and may spill sometimes",
            "Feeds themselves with their fingers",
            "Tries to use a spoon",
            "Climbs on and off a couch or chair without help",
        ),
        group(
            "2 years", 24,
            "Notices when others are hurt or upset",
            "Looks at your face to see how to react in a new situation",
            "Points to things in a book when you ask",
            "Says at least two words together, like \"More milk\"",
            "Points to at least two body parts when you ask",
            "Uses more gestures than just waving and pointing",
            "Holds something in one hand while using the other hand",
            "Tries to use switches, knobs, or buttons on a toy",
            "Plays with more than one toy at the same time",
            "Kicks a ball",
            "Runs",
            "Walks (not climbs) up a few stairs with or without help",
            "Eats with a spoon",
        ),
    )

    private val byKey = groups.flatMap { it.items }.associateBy { it.key }
    private val byTitle = groups.flatMap { it.items }.associateBy { normalize(it.title) }

    fun find(key: String?): Milestone? = key?.let(byKey::get)

    /** Matches imported milestones, which carry only a title. */
    fun findByTitle(title: String): Milestone? = byTitle[normalize(title)]

    private fun normalize(title: String) = title.lowercase().filter { it.isLetterOrDigit() }

    private fun group(label: String, months: Int, vararg titles: String) = MilestoneGroup(
        label = label,
        ageMonths = months,
        items = titles.mapIndexed { i, title -> Milestone("m${months}_${i + 1}", title) },
    )
}
