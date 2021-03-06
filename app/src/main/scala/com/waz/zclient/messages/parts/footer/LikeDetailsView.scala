/**
 * Wire
 * Copyright (C) 2018 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.waz.zclient.messages.parts.footer

import android.content.Context
import android.util.AttributeSet
import android.widget.{LinearLayout, TextView}
import com.waz.content.{ReactionsStorage, UsersStorage}
import com.waz.service.messages.MessageAndLikes
import com.waz.utils.events.Signal
import com.waz.zclient.utils.ContextUtils._
import com.waz.zclient.{R, ViewHelper}

class LikeDetailsView(context: Context, attrs: AttributeSet, style: Int) extends LinearLayout(context, attrs, style) with ViewHelper {
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)
  def this(context: Context) = this(context, null, 0)

  inflate(R.layout.message_footer_like_details)
  setOrientation(LinearLayout.HORIZONTAL)

  private val description: TextView = findById(R.id.like__description)

  def init(controller: FooterViewController): Unit =
    controller.messageAndLikes.flatMap {
        case MessageAndLikes(_, likes, _, _) if likes.isEmpty =>
          Signal.const(getString(R.string.message_footer__tap_to_like))
        case MessageAndLikes(_, likes, _, _) if likes.size > 3 =>
          Signal.const(getQuantityString(R.plurals.message_footer__number_of_likes, likes.size, Integer.valueOf(likes.size)))
        case MessageAndLikes(msg, _, _, _) =>
          for {
            reactionsStorage <- inject[Signal[ReactionsStorage]]
            likers           <- reactionsStorage.likes(msg.id).map(_.likers)
            usersStorage     <- inject[Signal[UsersStorage]]
            users            <- usersStorage.listSignal(likers.keys)
          } yield users.sortBy(u => likers(u.id)).map(_.name).mkString(", ")
    }.onUi(description.setText)
}

