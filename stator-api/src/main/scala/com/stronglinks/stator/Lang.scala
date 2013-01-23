
package com.stronglinks.stator

import twirl.api._

case class Lang(code: String, fallback: Option[Lang] = None) {

	def apply(content: String) = ContentBlock(Html(content), this)

	def apply(content: Html) = ContentBlock(content, this)

	def fallbacks: List[Lang] = fallback match {
		case None => Nil
		case Some(fb) => List(fb) ++ fb.fallbacks
	}
}

case class ContentBlock(html: Html, lang: Lang, next: Option[ContentBlock] = None) {

	def |(i: ContentBlock) = new ContentBlock(i.html, i.lang, Some(this))

	def apply()(implicit langInScope: Lang): Html =
		if(lang.code == langInScope.code) html
		else next match {
			case Some(next) => next()
			case None => 
				this()(lang.fallback.getOrElse(sys.error("no content in language " + langInScope)))
		}
}

object Stator extends Stator

trait Stator {

	val en = Lang("en")
	val fr = Lang("fr", Some(en))

	def when(lang: Lang)(c: Html)(implicit langInScope: Lang) =
		if(lang.code == langInScope.code) c
		else Html.empty
}
