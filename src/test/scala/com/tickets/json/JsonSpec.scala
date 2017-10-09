package com.tickets.json

import org.scalatest.{EitherValues, FlatSpec, Matchers}
import io.circe.parser._

class JsonSpec extends FlatSpec with Matchers with EitherValues {

  val inputJson =
    """{"movie_results":[{"adult":false,"backdrop_path":"/8uO0gUM8aNqYLs1OsTBQiXu0fEv.jpg",
    |"genre_ids":[18],"id":550,"original_language":"en","original_title":"Fight Club",
    |"overview":"A ticking-time-bomb insomniac and a slippery soap salesman channel primal male aggression
    |into a shocking new form of therapy. Their concept catches on, with underground \"fight clubs\" forming
    |in every town, until an eccentric gets in the way and ignites an out-of-control spiral toward oblivion.",
    |"release_date":"1999-10-15","poster_path":"/adw6Lq9FiC9zjYEpOqfq03ituwp.jpg","popularity":148.153306,
    |"title":"Fight Club","video":false,"vote_average":8.300000000000001,"vote_count":9597}],"person_results":[],
    |"tv_results":[],"tv_episode_results":[],"tv_season_results":[]}""".stripMargin.replaceAll("\n", "")

  "A Json" should "contain Fight Club as original_title" in {
    val parser = parse(inputJson).flatMap { json =>
      val cursor = json.hcursor
      cursor.downField("movie_results").downArray.first.get[String]("original_title")
    }

    parser.right.value should be ("Fight Club")
  }

}
