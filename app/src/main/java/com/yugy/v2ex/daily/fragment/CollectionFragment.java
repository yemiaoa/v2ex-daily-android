package com.yugy.v2ex.daily.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.astuetz.PagerSlidingTabStrip;
import com.yugy.v2ex.daily.R;
import com.yugy.v2ex.daily.activity.MainActivity;
import com.yugy.v2ex.daily.model.NodeModel;
import com.yugy.v2ex.daily.network.RequestManager;
import com.yugy.v2ex.daily.sdk.V2EX;
import com.yugy.v2ex.daily.utils.DebugUtils;
import com.yugy.v2ex.daily.widget.AppMsg;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

/**
 * Created by yugy on 14-2-25.
 */
public class CollectionFragment extends Fragment implements OnRefreshListener{

    private PagerSlidingTabStrip mPagerSlidingTabStrip;
    private ViewPager mViewPager;
    private TextView mEmptyText;

    private ArrayList<NodeModel> mCollectionNode;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_collection, container, false);
        mPagerSlidingTabStrip = (PagerSlidingTabStrip) rootView.findViewById(R.id.tab_fragment_collection);
        mViewPager = (ViewPager) rootView.findViewById(R.id.viewpager_fragment_collection);
        mEmptyText = (TextView) rootView.findViewById(R.id.txt_fragment_collection_empty);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mCollectionNode = new ArrayList<NodeModel>();
        V2EX.getAllNode(getActivity(), false, new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray jsonArray) {
                        DebugUtils.log(jsonArray);
                        try {
                            ArrayList<NodeModel> models = getModels(jsonArray);
                            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                            Set<String> collectionNodeId = sharedPreferences.getStringSet("node_collections", new HashSet<String>());
                            if(collectionNodeId.size() != 0){
                                for(NodeModel model : models){
                                    for(String id : collectionNodeId){
                                        if(String.valueOf(model.id).equals(id)){
                                            mCollectionNode.add(model);
                                        }
                                    }
                                }
                                mEmptyText.setVisibility(View.GONE);
                                mViewPager.setAdapter(new CollectionAdapter(getFragmentManager(), mCollectionNode));
                                mPagerSlidingTabStrip.setViewPager(mViewPager);
                            }
                        } catch (JSONException e) {
                            AppMsg.makeText(getActivity(), "Json decode error", AppMsg.STYLE_ALERT).show();
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        if (volleyError.getCause() instanceof EOFException) {
                            AppMsg.makeText(getActivity(), "Network error", AppMsg.STYLE_ALERT).show();
                        }
                        volleyError.printStackTrace();
                    }
                }
        );
    }

    private class CollectionAdapter extends FragmentStatePagerAdapter{

        private ArrayList<NodeModel> mModels;
        private ArrayList<String> mTitles;

        public CollectionAdapter(FragmentManager fm, ArrayList<NodeModel> models) {
            super(fm);
            mModels = models;
            mTitles = new ArrayList<String>();
            for(NodeModel model : mModels){
                mTitles.add(model.title);
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTitles.get(position);
        }

        @Override
        public Fragment getItem(int position) {
            NodeFragment nodeFragment = new NodeFragment();
            Bundle argument = new Bundle();
            argument.putInt("node_id", mModels.get(position).id);
            nodeFragment.setArguments(argument);
            return nodeFragment;
        }

        @Override
        public int getCount() {
            return mModels.size();
        }
    }

    private ArrayList<NodeModel> getModels(JSONArray jsonArray) throws JSONException {
        ArrayList<NodeModel> models = new ArrayList<NodeModel>();
        for(int i = 0; i < jsonArray.length(); i++){
            NodeModel model = new NodeModel();
            model.parse(jsonArray.getJSONObject(i));
            models.add(model);
        }
        return models;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(3);
    }

    @Override
    public void onDestroy() {
        RequestManager.getInstance().cancelRequests(getActivity());
        super.onDestroy();
    }

    @Override
    public void onRefreshStarted(View view) {

    }
}

