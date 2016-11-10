package com.stc.meetme;

/**
 * Created by artem on 11/10/16.
 */

public class Trash {
	/*
	private String getPlacesAndLocation() {
		Timber.w("getPlacesAndLocation");
		String result = "";
		Snackbar.make(mLayout,
				"Updating Places and Location",
				Snackbar.LENGTH_SHORT).show();

		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
				PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
					PERMISSION_REQUEST_FINE_LOCATION);
		} else {
			Awareness.SnapshotApi.getPlaces(mGoogleApiClient)
					.setResultCallback(new ResultCallback<PlacesResult>() {
						@Override
						public void onResult(@NonNull PlacesResult placesResult) {
							if (placesResult.getStatus().isSuccess()) {
								Timber.w("isSuccess");

								mProgressBar.setVisibility(View.GONE);
								List<PlaceLikelihood> placeLikelihood =
										placesResult.getPlaceLikelihoods();
								if (placeLikelihood != null && !placeLikelihood.isEmpty()) {
									String result = "";
									result+=placeLikelihood.get(0).getPlace().getName()+"\n";
									result+="accuracy: "+placeLikelihood.get(0).getLikelihood()+"\n";
									textPlaces.setText(result);
								} else {
									Timber.w("ERROR");

									Snackbar.make(mLayout,
											getString(R.string.error_no_places),
											Snackbar.LENGTH_LONG).show();
								}
							} else {
								Timber.w("ERROR2");

								mProgressBar.setVisibility(View.GONE);
								Snackbar.make(mLayout,
										getString(R.string.error_general),
										Snackbar.LENGTH_LONG).show();
							}
						}
					});
			Awareness.SnapshotApi.getLocation(mGoogleApiClient)
					.setResultCallback(new ResultCallback<LocationResult>() {
						@Override
						public void onResult(@NonNull LocationResult locationResult) {
							mProgressBar.setVisibility(View.GONE);
							if (locationResult.getStatus().isSuccess()) {
								Timber.w("success");

								String result = "";

								Location location = locationResult.getLocation();
								String locationText = getString(R.string.text_coordinates,
										location.getLatitude() + ", " + location.getLongitude());
								result+=locationText+"\n";
								result+="accuracy: "+location.getAccuracy()+"\n";
								result+="altitude: "+location.getAltitude()+"\n";
								textLocation.setText(result);

							} else {
								Timber.w("ERROR");

								Snackbar.make(mLayout,
										getString(R.string.error_general),
										Snackbar.LENGTH_LONG).show();
							}
						}
					});
		}
		return result;

	}



	private void getUserActivity() {
		Timber.w("getUserActivity");
		Snackbar.make(mLayout,
				"Updating activity",
				Snackbar.LENGTH_SHORT).show();

		if (ActivityCompat.checkSelfPermission(this, com.google.android.gms.permission.ACTIVITY_RECOGNITION) !=
				PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this,
					new String[]{com.google.android.gms.permission.ACTIVITY_RECOGNITION},
					PERMISSION_REQUEST_ACTIVITY_RECOGNITION);
		} else {
	Awareness.SnapshotApi.getDetectedActivity(mGoogleApiClient)
			.setResultCallback(new ResultCallback<DetectedActivityResult>() {
		@Override
		public void onResult(@NonNull DetectedActivityResult detectedActivityResult) {
			if (detectedActivityResult.getStatus().isSuccess()) {
				Timber.w("success");

				String result = "";

				ActivityRecognitionResult activityRecognitionResult =
						detectedActivityResult.getActivityRecognitionResult();

				long detectedActivity =
						activityRecognitionResult.getTime();
				String dateString = DateFormat.format("dd/MM/yyyy hh:mm:ss",
						new Date(detectedActivity)).toString();
				result+=dateString+"\n";

				long elapsedTime =
						activityRecognitionResult.getElapsedRealtimeMillis();
				String elapsed =
						DateFormat.format("hh:mm:ss", new Date(elapsedTime)).toString();
				result+="elapsed: "+elapsed+"\n";

				DetectedActivity mostProbableActivity =
						activityRecognitionResult.getMostProbableActivity();

				result+=getActivityString(mostProbableActivity.getType())+" "+
						mostProbableActivity.getConfidence();
				textActivity.setText(result);

			}else {
				Timber.w("ERROR");
				Snackbar.make(mLayout,
						getString(R.string.error_general),
						Snackbar.LENGTH_LONG).show();
			}
		}
	});
	//}

}*/
}
