package com.stc.meetme.model;

import java.io.Serializable;
import java.util.List;

/**
 * Created by artem on 11/10/16.
 */

public class UserStatus  implements Serializable {

	private List<ModelUserPosition> modelUserPosition;
	private ModelUserActivity modelUserActivity;

	public UserStatus() {
	}

	public List<ModelUserPosition> getModelUserPosition() {
		return modelUserPosition;
	}

	public void setModelUserPosition(List<ModelUserPosition> modelUserPosition) {
		this.modelUserPosition = modelUserPosition;
	}

	public ModelUserActivity getModelUserActivity() {

		return modelUserActivity;
	}

	public void setModelUserActivity(ModelUserActivity modelUserActivity) {
		this.modelUserActivity = modelUserActivity;
	}
}