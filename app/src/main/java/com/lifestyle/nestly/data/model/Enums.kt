package com.lifestyle.nestly.data.model

enum class ActivityType {
    BOTTLE_FEED,
    BREASTFEED,
    PUMP,
    DIAPER,
    GROWTH,
    MILESTONE,
    SLEEP,
    SOLIDS,
    ROUTINE,
    BABY_FIRST,
    MEDICAL,
    VACCINE,
}

enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK }

enum class Sex { MALE, FEMALE, OTHER }

enum class ProfileType { CHILD, PREGNANCY }

enum class MilkType { BREAST_MILK, FORMULA, MIXED }

enum class BreastSide { LEFT, RIGHT }

enum class DiaperType { WET, DIRTY, MIXED, DRY }

enum class StoolColor { YELLOW, GREEN, BROWN, BLACK, RED, WHITE, ORANGE }

enum class StoolTexture { RUN, MUSH, SOLID, MUCOUS, SEEDY, HARD }

enum class ReminderKind { FEED, PUMP, DIAPER, SLEEP }
