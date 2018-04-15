class User < ApplicationRecord
  devise :omniauthable, :omniauth_providers => [:facebook]
end
