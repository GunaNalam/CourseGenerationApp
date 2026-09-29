1. Let's use spring boot here(Maintain a common API spec file here)
2. Let's not only see it as a hackathon project but in general on me understanding the subject, let's use typescript and remember we used it for extendability purpose only 
3. Let's go with free one, yeah let's go ahead with hybrid(remember we should put these in such a way in code that's I can easily change to a other no sql, sql database)
4. Let's go with Rest + versioning and why is GraphQL exactly needed here? Can you touch upon on this a little ??
5. Let's use the auth0 - if it's paid let's host something on our self
6. Single provider hardcoded - Let's use a file where it contains the json or something which has this prompt and I want the prompt in such a way that first it gives a history(to keep this short and bit basic), next is the course like all the a-z(with proper examples where ever required), and last maybe a real life application(if applicable). THe goal is to not just generate some random thing but a good resource where someone can genuiely understand from our course. You can also checkout for my style from /Users/gunanalam/Documents/Temp/go.md. Let's use the queue only, we can get better control and use the states wherever required so that user has visibility(do you want to display the queue positions as well - using some kind of sql query)
7. It's fine to keep it open, like a json object not restrict to some fields(what do you think) ??
8. Yeah let's use the Serverless functions per step, mainly due to free resources 
9. None initially 
10. Use everything required to meet the milestone(add these in prompt to the AI as well)
11. Give a retry logic in case something fails.
12. Yeah let's have some unit test cases, intergration testing if that's the expectations
13. Is this really required(if so let's have different admin view it self) and probably have very basic things like current messages etc or similar ones
14. I didn't get this part but our's will be .env and single repos only and vercel automatically deploys the changes right? Should we do something here ??

Extendable: 
1. Yes definately, if someone gives then we store it wrt the user else, we use the default one configured - also clearly show that you token is expired in that case and using a default key(ours). Have some kind of basic encryption for now later we can say the scope here
2. Yeah please 
3. Great we can show these steps(configure these exactly in backend as well), how are we planning to use queue and maintain these a single thing and also it will like one's response will be other's input as well right? How do we handle the dependency of each requests 
4. That's fine
5. Let's use the last 1min window and process max of 2 request(you are aware of this approach, right?)
6. We can put it for future scope here(to less complicate things)
7. Yeah please that's a good thing - have a api to return the stored one.

---> must stay on free tiers with minimal 
---> Mostly a hackthon 
---> Let's be a single user(is there any limitation? Let's keep it in such a way multi user is also supported)

Let's cover all the milestones, we should meet them and not comprise on anything - ok? 
