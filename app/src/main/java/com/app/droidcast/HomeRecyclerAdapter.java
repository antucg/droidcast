package com.app.droidcast;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.app.droidcast.models.CastItem;
import com.app.droidcast.models.CastItemList;

/**
 * Adapter for the recycler view in the home activity.
 */

public class HomeRecyclerAdapter extends RecyclerView.Adapter<HomeRecyclerAdapter.ViewHolder> {

  // List of items to render
  private CastItemList modelData;

  /**
   * Constructor.
   *
   * @param castItemList List of items to render.
   */
  public HomeRecyclerAdapter(CastItemList castItemList) {
    modelData = castItemList;
  }

  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.castitemlist_row, parent, false);
    return new ViewHolder(view);
  }

  @Override public void onBindViewHolder(ViewHolder holder, int position) {
    CastItem castItem = modelData.getCastItemList().get(position);

    // If label is empty, use default empty message
    if (castItem.getLabel() == null) {
      holder.textView.setText(R.string.home_recycler_empty);
    } else {
      holder.textView.setText(castItem.getLabel());
    }

    //  TODO: set image depending on resource type
  }

  @Override public int getItemCount() {
    return modelData.size();
  }

  static class ViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.home_recycler_resource_type) ImageView imageView;
    @BindView(R.id.home_recycler_label) TextView textView;

    public ViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }

    /**
     * Handler for cast button click event.
     *
     * @param v Clicked view.
     */
    @OnClick(R.id.home_recycler_cast_button) public void onCastButtonClick(View v) {
      System.out.println("Cast button clicked");
      v.getContext().startActivity(MediaShareActivity.createIntent(v.getContext()));
    }
  }
}
