package com.jpaver.trianglelist


import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.`is`
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest2 {

    @Rule
    @JvmField
    var mActivityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Rule
    @JvmField
    var mGrantPermissionRule =
        GrantPermissionRule.grant(
            "android.permission.INTERNET",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"
        )

    @Test
    fun mainActivityTest2() {
        val appCompatEditText = onView(
            allOf(
                withId(R.id.editLengthB1),
                childAtPosition(
                    allOf(
                        withId(R.id.LL3),
                        childAtPosition(
                            withId(R.id.editTextLayout),
                            3
                        )
                    ),
                    3
                ),
                isDisplayed()
            )
        )
        appCompatEditText.perform(replaceText("6"), closeSoftKeyboard())

        val appCompatEditText2 = onView(
            allOf(
                withId(R.id.editLengthB1), withText("6"),
                childAtPosition(
                    allOf(
                        withId(R.id.LL3),
                        childAtPosition(
                            withId(R.id.editTextLayout),
                            3
                        )
                    ),
                    3
                ),
                isDisplayed()
            )
        )
        appCompatEditText2.perform(pressImeActionButton())

        val appCompatEditText3 = onView(
            allOf(
                withId(R.id.editLengthC1),
                childAtPosition(
                    allOf(
                        withId(R.id.LL3),
                        childAtPosition(
                            withId(R.id.editTextLayout),
                            3
                        )
                    ),
                    4
                ),
                isDisplayed()
            )
        )
        appCompatEditText3.perform(replaceText("6"), closeSoftKeyboard())

        val floatingActionButton = onView(
            allOf(
                withId(R.id.fab_replace), withContentDescription("rewrite shape"),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("androidx.coordinatorlayout.widget.CoordinatorLayout")),
                        0
                    ),
                    18
                ),
                isDisplayed()
            )
        )
        floatingActionButton.perform(click())

        val floatingActionButton2 = onView(
            allOf(
                withId(R.id.fab_dimsideh),
                withContentDescription("flip dimension text in vertical"),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("androidx.coordinatorlayout.widget.CoordinatorLayout")),
                        0
                    ),
                    21
                ),
                isDisplayed()
            )
        )
        floatingActionButton2.perform(click())

        val floatingActionButton3 = onView(
            allOf(
                withId(R.id.fab_dimsidew),
                withContentDescription("flip dimension text in horizonal"),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("androidx.coordinatorlayout.widget.CoordinatorLayout")),
                        0
                    ),
                    20
                ),
                isDisplayed()
            )
        )
        floatingActionButton3.perform(click())

        val floatingActionButton4 = onView(
            allOf(
                withId(R.id.fab_flag), withContentDescription("flag up a information text"),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("androidx.coordinatorlayout.widget.CoordinatorLayout")),
                        0
                    ),
                    23
                ),
                isDisplayed()
            )
        )
        floatingActionButton4.perform(click())

        val floatingActionButton5 = onView(
            allOf(
                withId(R.id.fab_up), withContentDescription("list to up"),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("androidx.coordinatorlayout.widget.CoordinatorLayout")),
                        0
                    ),
                    6
                ),
                isDisplayed()
            )
        )
        floatingActionButton5.perform(click())

        val floatingActionButton6 = onView(
            allOf(
                withId(R.id.fab_resetView), withContentDescription("reset view"),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("androidx.coordinatorlayout.widget.CoordinatorLayout")),
                        0
                    ),
                    5
                ),
                isDisplayed()
            )
        )
        floatingActionButton6.perform(click())
    }

    private fun childAtPosition(
        parentMatcher: Matcher<View>, position: Int
    ): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
