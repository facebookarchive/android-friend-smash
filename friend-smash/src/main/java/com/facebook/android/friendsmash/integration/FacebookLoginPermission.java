/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.android.friendsmash.integration;

/**
 * Enum listing Login permissions used in Friend Smash!
 * For full list of permissions see https://developers.facebook.com/docs/facebook-login/permissions/
 */
public enum FacebookLoginPermission {

    USER_FRIENDS("user_friends", true),
    PUBLISH_ACTIONS("publish_actions", false);

    private String permisison;
    private boolean isRead;

    FacebookLoginPermission (String permission, boolean isRead) {
        this.permisison = permission;
        this.isRead = isRead;
    }

    /**
     * Checks if the permission is a read permission or write permission. In general read permissions
     * Allow apps to read information about the user who grants them, write permissions allow apps
     * to change user information on their behalf, for example publish_actions permission allows
     * app to post on behalf of the user. It's needed because in some scenarios SDK provides
     * different methods for working with read and write permissions.
     */
    public boolean isRead() {return isRead;}

    /**
     * String representation of the permission, as defined in
     * https://developers.facebook.com/docs/facebook-login/permissions/
     */
    @Override
    public String toString() {
        return permisison;
    }
}
