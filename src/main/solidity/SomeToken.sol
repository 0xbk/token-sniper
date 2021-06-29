import "./IERC20.sol";

abstract contract SomeToken is IERC20  {
    uint256 public _maxTxAmount;
    address public uniswapV2Pair;
    uint256 public  _liquidityFee = 5; // kept for liquidity
    uint256 public  _marketingFee = 2; // marketing wallet
    uint256 public  _burnFee      = 1; // burned
    uint256 public  _potFee       = 2; // pot fees

    // uint256 public sellLimit;
    // bool public tradingEnabled;
    // function getSellLockTimeInSeconds() public virtual view returns(uint256);
}
