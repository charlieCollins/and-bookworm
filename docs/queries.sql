select book.*, bookuserdata.rstat, bookuserdata.rat, bookuserdata.blurb from book join bookuserdata on book.bid = bookuserdata.bid;

select book.*, bookuserdata.rstat, bookuserdata.rat, bookuserdata.blurb from book join bookuserdata on book.bid = bookuserdata.bid order by bookuserdata.rat asc;
