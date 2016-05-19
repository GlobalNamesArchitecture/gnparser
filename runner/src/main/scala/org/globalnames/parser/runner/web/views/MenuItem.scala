package org.globalnames.parser.runner.web.views

case class MenuItemContent(caption: String, route: String)

case class MenuItem(content: MenuItemContent, current: Boolean)
