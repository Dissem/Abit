/*
 * Copyright 2015 Christian Basler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.dissem.apps.abit.utils;

import android.content.Context;
import android.view.Menu;
import ch.dissem.apps.abit.R;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

/**
 * Some helper methods to work with drawables.
 */
public class Drawables {
    public static void addIcon(Context ctx, Menu menu, int menuItem, GoogleMaterial.Icon icon) {
        menu.findItem(menuItem).setIcon(new IconicsDrawable(ctx, icon).colorRes(R.color.primary_text_default_material_dark).actionBar());
    }
}
