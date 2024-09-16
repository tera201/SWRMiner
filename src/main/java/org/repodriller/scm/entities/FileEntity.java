package org.repodriller.scm.entities;

import lombok.Getter;

@Getter
public class FileEntity {

	public final int projectId;
	public final String filePath;
	public final Long filePathId;
	public final String hash;
	public final int date;

	public FileEntity(int projectId, String filePath, Long filePathId, String hash, int date) {
		this.projectId = projectId;
		this.filePath = filePath;
		this.filePathId = filePathId;
		this.hash = hash;
		this.date = date;
	}

	@Override
	public String toString() {
		return "File [projectId=" + projectId + ", filePath=" + filePath + ", hash=" + hash + ", date="
				+ date + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + projectId;
		result = prime * result + ((filePath == null) ? 0 : filePath.hashCode());
		result = prime * result + ((hash == null) ? 0 : hash.hashCode());
		result = prime * result + date;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileEntity other = (FileEntity) obj;
		if (filePath == null) {
			if (other.filePath != null)
				return false;
		} else if (!filePath.equals(other.filePath))
			return false;
		if (hash == null) {
			if (other.hash != null)
				return false;
		} else if (!hash.equals(other.hash))
			return false;
		if (date != other.date) {
				return false;
		}
		if (projectId != other.projectId)
			return false;
		return true;
	}
}
