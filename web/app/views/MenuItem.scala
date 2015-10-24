package views

import play.api.mvc.Call

case class MenuItemContent(caption: String, route: Call)

case class MenuItem(content: MenuItemContent, current: Boolean)
