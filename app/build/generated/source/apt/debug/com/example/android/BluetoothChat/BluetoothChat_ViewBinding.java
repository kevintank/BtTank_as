// Generated code from Butter Knife. Do not modify!
package com.example.android.BluetoothChat;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import android.widget.ImageButton;
import butterknife.Unbinder;
import butterknife.internal.DebouncingOnClickListener;
import butterknife.internal.Utils;
import java.lang.IllegalStateException;
import java.lang.Override;

public class BluetoothChat_ViewBinding implements Unbinder {
  private BluetoothChat target;

  private View view2131165227;

  private View view2131165228;

  private View view2131165229;

  private View view2131165224;

  private View view2131165226;

  private View view2131165221;

  private View view2131165222;

  @UiThread
  public BluetoothChat_ViewBinding(BluetoothChat target) {
    this(target, target.getWindow().getDecorView());
  }

  @UiThread
  public BluetoothChat_ViewBinding(final BluetoothChat target, View source) {
    this.target = target;

    View view;
    view = Utils.findRequiredView(source, R.id.button_ro_left, "field 'button_ro_left' and method 'TurretLeft'");
    target.button_ro_left = Utils.castView(view, R.id.button_ro_left, "field 'button_ro_left'", ImageButton.class);
    view2131165227 = view;
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.TurretLeft();
      }
    });
    view = Utils.findRequiredView(source, R.id.button_ro_right, "field 'button_ro_right' and method 'TurretRight'");
    target.button_ro_right = Utils.castView(view, R.id.button_ro_right, "field 'button_ro_right'", ImageButton.class);
    view2131165228 = view;
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.TurretRight();
      }
    });
    view = Utils.findRequiredView(source, R.id.button_ro_stop, "field 'button_ro_stop' and method 'TurretStop'");
    target.button_ro_stop = Utils.castView(view, R.id.button_ro_stop, "field 'button_ro_stop'", ImageButton.class);
    view2131165229 = view;
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.TurretStop();
      }
    });
    view = Utils.findRequiredView(source, R.id.button_left_up, "field 'button_left_up' and method 'SteeringLeftUp'");
    target.button_left_up = Utils.castView(view, R.id.button_left_up, "field 'button_left_up'", ImageButton.class);
    view2131165224 = view;
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.SteeringLeftUp();
      }
    });
    view = Utils.findRequiredView(source, R.id.button_right_up, "field 'button_right_up' and method 'SteeringRightUp'");
    target.button_right_up = Utils.castView(view, R.id.button_right_up, "field 'button_right_up'", ImageButton.class);
    view2131165226 = view;
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.SteeringRightUp();
      }
    });
    view = Utils.findRequiredView(source, R.id.button_down_left, "field 'button_down_left' and method 'SteeringDownLeft'");
    target.button_down_left = Utils.castView(view, R.id.button_down_left, "field 'button_down_left'", ImageButton.class);
    view2131165221 = view;
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.SteeringDownLeft();
      }
    });
    view = Utils.findRequiredView(source, R.id.button_down_right, "field 'button_down_right' and method 'SteeringDownRight'");
    target.button_down_right = Utils.castView(view, R.id.button_down_right, "field 'button_down_right'", ImageButton.class);
    view2131165222 = view;
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.SteeringDownRight();
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
    target.button_left_up = null;
    target.button_right_up = null;
    target.button_down_left = null;
    target.button_down_right = null;

    view2131165227.setOnClickListener(null);
    view2131165227 = null;
    view2131165228.setOnClickListener(null);
    view2131165228 = null;
    view2131165229.setOnClickListener(null);
    view2131165229 = null;
    view2131165224.setOnClickListener(null);
    view2131165224 = null;
    view2131165226.setOnClickListener(null);
    view2131165226 = null;
    view2131165221.setOnClickListener(null);
    view2131165221 = null;
    view2131165222.setOnClickListener(null);
    view2131165222 = null;
  }
}
