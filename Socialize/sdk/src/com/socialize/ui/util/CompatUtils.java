package com.socialize.ui.util;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.Window;

/**
 * @author Jason Polites
 */
public class CompatUtils {
	public static void setBackgroundDrawable(View view, Drawable bg) {
		if(android.os.Build.VERSION.SDK_INT < 16) {
			view.setBackgroundDrawable(bg);
		}
		else {
			view.setBackgroundDrawable(bg);
		}
	}
}
