-- 优惠券ID以及用户id,订单ID
local vocherId = ARGV[1]
local userId = ARGV[2]
local orderId = ARGV[3]

-- 库存key，保存用户key
local stockKey = 'seckill:stock:' .. vocherId
local userKey = 'seckill:user:' .. vocherId
-- 判断库存是否充足
if(tonumber(redis.call('get',stockKey)) <= 0) then
    return 1
end
-- 判断用户是否已经领取过
if(redis.call('sismember',userKey,userId) == 1) then
    return 2
end

-- 库存减1
redis.call('incrby',stockKey,-1)
-- 添加用户
redis.call('sadd',userKey,userId)
-- 发送消息到stream队列中
redis.call('xadd','stream.orders','*','vocherId',vocherId,'userId',userId,'id',orderId);
return 0