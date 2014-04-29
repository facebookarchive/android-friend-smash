/**
 * Copyright 2012 Facebook
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.android.friendsmash;

/**
 *  Class representing an individual scoreboard entry
 */
public class ScoreboardEntry implements Comparable<ScoreboardEntry> {

	// Attributes for a ScoreboardEntry
	private String id;
	private String name;
	private int score;
	
	public ScoreboardEntry (String id, String name, int score) {
		setId(id);
		setName(name);
		setScore(score);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	@Override
	public int compareTo(ScoreboardEntry another) {
		// Returns a negative integer, zero, or a positive integer as this object
		// is less than, equal to, or greater than the specified object.
		return this.score - another.score;
	}
	
}
