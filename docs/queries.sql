select book.*, bookuserdata.rstat, bookuserdata.rat, bookuserdata.blurb from book join bookuserdata on book.bid = bookuserdata.bid;

select book.*, bookuserdata.rstat, bookuserdata.rat, bookuserdata.blurb from book join bookuserdata on book.bid = bookuserdata.bid order by bookuserdata.rat asc;

select book.tit, group_concat(author.name) as authors from book join bookauthor on bookauthor.bid = book.bid join author on author.aid = bookauthor.aid group by book.bid;

select book.bid as _id, book.tit, book.subtit, book.pub, book.datepub, book.format, bookuserdata.rstat, bookuserdata.rat, bookuserdata.blurb, group_concat(author.name) as authors from book join bookuserdata on book.bid = bookuserdata.bid join bookauthor on bookauthor.bid = book.bid join author on author.aid = bookauthor.aid group by book.bid
  
