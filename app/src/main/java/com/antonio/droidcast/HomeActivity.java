package com.antonio.droidcast;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.antonio.droidcast.dao.DaoException;
import com.antonio.droidcast.dao.DaoFactory;
import com.antonio.droidcast.ioc.IOCProvider;
import com.antonio.droidcast.models.CastItemList;
import javax.inject.Inject;

public class HomeActivity extends BaseActivity {

  @BindView(R.id.home_recycler_view) RecyclerView homeRecyclerView;
  private RecyclerView.LayoutManager recyclerLayoutManager;
  private RecyclerView.Adapter recyclerAdapter;

  @Inject DaoFactory daoFactory;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_home);

    ButterKnife.bind(this);
    IOCProvider.getInstance().inject(this);

    setupRecyclerView();
  }

  /**
   * Setup recycler view, layout manater and adapter.
   */
  private void setupRecyclerView() {
    // Setup recycler view
    homeRecyclerView.setHasFixedSize(true);
    recyclerLayoutManager = new LinearLayoutManager(this);
    homeRecyclerView.setLayoutManager(recyclerLayoutManager);

    CastItemList castItemList = new CastItemList();
    try {
      castItemList = daoFactory.get(CastItemList.class).get();
    } catch (DaoException e) {
      Log.e(TAG, "[HomeActivity] - onCreate(), error reading CastItemList from storage");
    }

    recyclerAdapter = new HomeRecyclerAdapter(castItemList);
    homeRecyclerView.setAdapter(recyclerAdapter);
  }
}
