--
-- Filter to deduplicate DataWithSource records coming from the same Face Detection tracks.
--
function pred(x, y) 
	return ( x.FaceResultAndImage.id.uuid == y.FaceResultAndImage.id.uuid ) 
end
