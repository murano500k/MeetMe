package com.stc.meetme.ui;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.stc.meetme.R;

import static com.stc.meetme.Constants.ARG_OBSERVE_ID;

public class FragmentMap extends SupportMapFragment {
	private String mObserveUid;
	private static final String TAG = "FragmentMap";
	private GoogleMap mMap;
	LatLng observeLatLng;
	Marker marker;
	private OnFragmentInteractionListener mListener;

	public FragmentMap() {

	}

	public static FragmentMap newInstance(String observeId) {
		FragmentMap fragment = new FragmentMap();
		Bundle args = new Bundle();
		args.putString(ARG_OBSERVE_ID, observeId);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {
			mObserveUid = getArguments().getString(ARG_OBSERVE_ID);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_map, container, false);
		return view;
	}


	public void onButtonPressed(Uri uri) {
		if (mListener != null) {
			mListener.onFragmentInteraction(uri);
		}
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		if (context instanceof OnFragmentInteractionListener) {
			mListener = (OnFragmentInteractionListener) context;
		} else {
			throw new RuntimeException(context.toString()
					+ " must implement OnFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}

	public interface OnFragmentInteractionListener {
		// TODO: Update argument type and name
		void onFragmentInteraction(Uri uri);
	}
}
