// Generated code from Butter Knife. Do not modify!
package com.example.android.BluetoothChat;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import android.widget.Button;
import butterknife.Unbinder;
import butterknife.internal.DebouncingOnClickListener;
import butterknife.internal.Utils;
import java.lang.IllegalStateException;
import java.lang.Override;

public class BluetoothChat_ViewBinding implements Unbinder {
  private BluetoothChat target;

  private View view2131165217;

  private View view2131165218;

  private View view2131165219;

  @UiThread
  public BluetoothChat_ViewBinding(BluetoothChat target) {
    this(target, target.getWindow().getDecorView());
  }

  @UiThread
  public BluetoothChat_ViewBinding(final BluetoothChat target, View source) {
    this.target = target;

    View view;
    view = Utils.findRequiredView(source, R.id.button_ro_left, "field 'button_ro_left' and method 'TurretLeft'");
    target.button_ro_left = Utils.castView(view, R.id.button_ro_left, "field 'button_ro_left'", Button.class);
    view2131165217 = view;
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.TurretLeft();
      }
    });
    view = Utils.findRequiredView(source, R.id.button_ro_right, "field 'button_ro_right' and method 'TurretRight'");
    target.button_ro_right = Utils.castView(view, R.id.button_ro_right, "field 'button_ro_right'", Button.class);
    view2131165218 = view;
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.TurretRight();
      }
    });
    view = Utils.findRequiredView(source, R.id.button_ro_stop, "field 'button_ro_stop' and method 'TurretStop'");
    target.button_ro_stop = Utils.castView(view, R.id.button_ro_stop, "field 'button_ro_stop'", Button.class);
    view2131165219 = view;
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.TurretStop();
      }
    });
  }

  @Override
  @CallSuper
  public void unbind() {
    BluetoothChat target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.button_ro_left = null;
    target.button_ro_right = null;
    target.button_ro_stop = null;

    view2131165217.setOnClickListener(null);
    view2131165217 = null;
    view2131165218.setOnClickListener(null);
    view2131165218 = null;
    view2131165219.setOnClickListener(null);
    view2131165219 = null;
  }
}
