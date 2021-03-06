package com.waz.zclient.participants.fragments

import android.os.Bundle
import androidx.recyclerview.widget.{LinearLayoutManager, RecyclerView}
import com.waz.log.BasicLogging.LogTag.DerivedLogTag
import com.waz.model.{ConversationRole, UserId}
import com.waz.service.ZMessaging
import com.waz.threading.{CancellableFuture, Threading}
import com.waz.utils.events.Signal
import com.waz.utils.returning
import com.waz.zclient.common.controllers.ThemeController
import com.waz.zclient.controllers.navigation.Page
import com.waz.zclient.participants.UserRequester
import com.waz.zclient.utils.StringUtils
import com.waz.zclient.R
import com.waz.zclient.pages.main.MainPhoneFragment
import com.waz.zclient.pages.main.pickuser.controller.IPickUserController
import scala.concurrent.duration._

abstract class UntabbedRequestFragment extends SingleParticipantFragment with DerivedLogTag {
  import UntabbedRequestFragment._
  import Threading.Implicits.Ui
  
  protected val Tag: String

  override protected val layoutId: Int = R.layout.fragment_participants_not_tabbed

  protected lazy val userToConnectId = UserId(getArguments.getString(ArgumentUserId))
  protected lazy val removeMemberPermission = participantsController.selfRole.map(_.canRemoveGroupMember)
  
  private lazy val userRequester = UserRequester.valueOf(getArguments.getString(ArgumentUserRequester))
  protected lazy val fromParticipants = userRequester == UserRequester.PARTICIPANTS
  protected lazy val fromDeepLink     = userRequester == UserRequester.DEEP_LINK
  
  override protected def initViews(savedInstanceState: Bundle): Unit = {
    detailsView
    footerMenu
  }
  
  override protected lazy val detailsView = returning( view[RecyclerView](R.id.not_tabbed_recycler_view) ) { vh =>
    vh.foreach(_.setLayoutManager(new LinearLayoutManager(ctx)))
    
    (for {
        zms           <- inject[Signal[ZMessaging]].head
        Some(user)    <- participantsController.getUser(userToConnectId)
        isGroup       <- participantsController.isGroup.head
        isGuest       =  !user.isWireBot && user.isGuest(zms.teamId)
        isExternal    =  !user.isWireBot && user.isExternal(zms.teamId)
        isDarkTheme   <- inject[ThemeController].darkThemeSet.head
        isWireless    =  user.expiresAt.isDefined
      } yield (user, isGuest, isExternal, isDarkTheme, isGroup, isWireless)).foreach {
        case (user, isGuest, isExternal, isDarkTheme, isGroup, isWireless) =>
          val formattedHandle = StringUtils.formatHandle(user.handle.map(_.string).getOrElse(""))
          val participantRole = participantsController.participants.map(_.get(userToConnectId))
          val selfRole =
            if (fromParticipants)
              participantsController.selfRole.map(Option(_))
            else
              Signal.const(Option.empty[ConversationRole])

          val adapter = new UnconnectedParticipantAdapter(user.id, isGuest, isExternal, isDarkTheme, isGroup, isWireless, user.name, formattedHandle)
          subs += Signal(timerText, participantRole, selfRole).onUi {
            case (tt, pRole, sRole) => adapter.set(tt, pRole, sRole)
          }
          subs += adapter.onParticipantRoleChange.on(Threading.Background)(participantsController.setRole(user.id, _))
          vh.foreach(_.setAdapter(adapter))
      }
  }

  protected lazy val returnPage = userRequester match {
    case UserRequester.SEARCH => Page.PICK_USER
    case _                    => Page.CONVERSATION_LIST
  }

  override def onBackPressed(): Boolean = {
    inject[IPickUserController].hideUserProfile()
    if (fromParticipants) participantsController.selectedParticipant ! None

    if (fromDeepLink) {
      CancellableFuture.delay(750.millis).map { _ =>
        navigationController.setVisiblePage(Page.CONVERSATION_LIST, MainPhoneFragment.Tag)
      }
    } else navigationController.setLeftPage(returnPage, Tag)
    false
  }
}

object UntabbedRequestFragment {
  val ArgumentUserId = "ARGUMENT_USER_ID"
  val ArgumentUserRequester = "ARGUMENT_USER_REQUESTER"
}