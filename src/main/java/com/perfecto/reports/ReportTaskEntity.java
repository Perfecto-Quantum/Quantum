package com.perfecto.reports;

public class ReportTaskEntity {
	
	public static enum TaskStatus {
		IN_PROGRESS, COMPLETE
	}
	
	private String taskId;
	private TaskStatus status;
	private String url;

	@SuppressWarnings("unused")
	public ReportTaskEntity() {
	}

	public String getTaskId() {
		return taskId;
	}

	@SuppressWarnings("unused")
	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}

	public TaskStatus getStatus() {
		return status;
	}

	@SuppressWarnings("unused")
	public void setStatus(TaskStatus status) {
		this.status = status;
	}

	public String getUrl() {
		return url;
	}

	@SuppressWarnings("unused")
	public void setUrl(String url) {
		this.url = url;
	}

}
