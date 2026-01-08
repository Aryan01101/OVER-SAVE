package com.example.budgettracker.service;

import java.util.List;

public final class SystemCategoryDefaults {

    private SystemCategoryDefaults() {}

    public static final List<String> NAMES = List.of(
            "Uncategorized",
            "Income",
            "Food",
            "Groceries",
            "Transport",
            "Shopping",
            "Entertainment",
            "Education",
            "Health",
            "Fitness",
            "Housing",
            "Utilities",
            "Other"
    );
}
