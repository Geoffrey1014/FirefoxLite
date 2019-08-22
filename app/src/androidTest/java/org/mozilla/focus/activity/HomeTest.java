/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.content.Context;
import android.content.Intent;
import android.util.DisplayMetrics;

import androidx.annotation.Keep;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.action.Tap;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.rule.ActivityTestRule;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.autobot.BottomBarRobot;
import org.mozilla.focus.helper.SessionLoadedIdlingResource;
import org.mozilla.focus.history.model.Site;
import org.mozilla.focus.utils.AndroidTestUtils;
import org.mozilla.focus.utils.TopSitesUtils;
import org.mozilla.rocket.content.ExtentionKt;

import java.util.List;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressBack;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.mozilla.focus.utils.RecyclerViewTestUtils.atPosition;

@Keep
@RunWith(AndroidJUnit4.class)
public class HomeTest {

    // Defer the startup of the activity cause we want to avoid First Run / Share App / Rate App dialogs
    @Rule
    public final ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class, true, false);
    private SessionLoadedIdlingResource loadingIdlingResource;

    @Before
    public void setUp() {
        // Set the share preferences and start the activity
        AndroidTestUtils.beforeTest();
    }

    @Test
    @Ignore("fix this after top sites implemented")
    public void clickTopSite_loadTopSite() {

        // Now start the activity
        activityRule.launchActivity(new Intent());
        loadingIdlingResource = new SessionLoadedIdlingResource(activityRule.getActivity());

        final MainActivity context = activityRule.getActivity();

        try {
            // Get test top sites
            String topSitesJsonString = ExtentionKt.appComponent((Context) getApplicationContext())
                    .topSitesRepo().getDefaultTopSitesJsonString();
            final JSONArray jsonDefault = new JSONArray(topSitesJsonString);
            final List<Site> defaultSites = TopSitesUtils.paresJsonToList(context, jsonDefault);

            // Check the title of the sample top site is correct
            onView(withId(R.id.main_list))
                    .check(matches(atPosition(0, hasDescendant(withText(defaultSites.get(0).getTitle())))));

            // Click and load the sample top site
            // Some intermittent issues happens when performing a single click event, we add a rollback action in case of a long click action
            // is triggered unexpectedly here. i.e. pressBack() can dismiss the popup menu.
            onView(ViewMatchers.withId(R.id.main_list))
                    .perform(RecyclerViewActions.actionOnItemAtPosition(0, click(pressBack())));

            // After page loading completes
            IdlingRegistry.getInstance().register(loadingIdlingResource);

            // Check if the url is displayed correctly
            onView(withId(R.id.display_url))
                    .check(matches(allOf(withText(defaultSites.get(0).getUrl()), isDisplayed())));

            // Always remember to unregister idling resource
            IdlingRegistry.getInstance().unregister(loadingIdlingResource);

        } catch (JSONException e) {
            e.printStackTrace();
            throw new AssertionError("testTopSite failed:", e);

        }
    }

    /**
     * Test case no: TC_00006
     * Test case name: Home page basic assets
     * Steps:
     * 1. Launch Rocket
     * 2. check visible - project name, search bar, top sites, tab tray
     */
    @Test
    @Ignore("waiting for coroutine to finish in order to show main_list")
    public void checkBasicHomeComponents_allAreVisible() {
        activityRule.launchActivity(new Intent());

        // Check if App Logo is visible
        onView(withId(R.id.home_fragment_title)).check(matches(isDisplayed()));

        // Check if home search field is visible
        onView(withId(R.id.home_fragment_fake_input)).check(matches(isDisplayed()));

        // Check if top site list is visible
        onView(withId(R.id.main_list)).check(matches(isDisplayed()));

        // Check if menu button is visible
        onView(withId(R.id.home_fragment_menu_button)).check(matches(isDisplayed()));

        // Check if tab tray button is visible
        onView(withId(R.id.home_fragment_tab_counter)).check(matches(isDisplayed()));
    }

    /**
     * Test case no: TC0039
     * Test case name: Dismiss menu
     * Steps:
     * 1. Launch app
     * 2. Tap menu
     * 3. Press back key
     * 4. Check menu panel not exist
     * 5. Tap menu
     * 6. Tap on blank space above menu or
     * 7. Check menu panel not exist
     */
    @FlakyTest
    @Test
    public void dismissMenu() {
        activityRule.launchActivity(new Intent());

        // Tap menu
        AndroidTestUtils.tapHomeMenuButton();

        // Press back
        Espresso.pressBack();

        // Check menu panel not exist
        onView(withId(R.id.menu)).check(doesNotExist());

        // Tap menu
        AndroidTestUtils.tapHomeMenuButton();

        // Tap on blank space above menu
        final DisplayMetrics displayMetrics = activityRule.getActivity().getResources().getDisplayMetrics();
        final int displayWidth = displayMetrics.widthPixels;
        final int displayHeight = displayMetrics.heightPixels;
        onView(withId(R.id.menu)).perform(AndroidTestUtils.clickXY(displayWidth / 2, -displayHeight / 2, Tap.SINGLE));

        //  Check menu panel not exist
        onView(withId(R.id.menu)).check(doesNotExist());
    }
}